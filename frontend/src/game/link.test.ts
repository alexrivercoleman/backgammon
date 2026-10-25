import { describe, expect, it } from 'vitest'
import { linkedGameId, linkTo } from './link'

const id = '3f2b8c1e-9d4a-4e7b-a1c2-5d6e7f8a9b0c'

describe('linkedGameId', () => {
  it('reads the game id', () => {
    expect(linkedGameId(`?game=${id}`)).toBe(id)
  })

  it('is null without one', () => {
    expect(linkedGameId('')).toBeNull()
    expect(linkedGameId('?other=1')).toBeNull()
  })

  it('ignores anything that is not a game id', () => {
    expect(linkedGameId('?game=../health')).toBeNull()
    expect(linkedGameId('?game=')).toBeNull()
  })
})

describe('linkTo', () => {
  it('sets the game id', () => {
    expect(linkTo('', id)).toBe(`?game=${id}`)
  })

  it('replaces the old id and keeps other parameters', () => {
    expect(linkTo(`?lang=el&game=${id}`, 'new')).toBe('?lang=el&game=new')
  })
})
